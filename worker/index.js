/*
 * The web app's Worker.
 *
 * Two jobs, and it keeps them apart. Static assets are the game itself, one HTML file, which
 * Cloudflare serves before this script is ever entered. Everything left over is the relay that
 * lets two people find each other, which the desktop build does over the LAN and a page cannot.
 *
 * The relay is deliberately thin. It stores nothing, inspects nothing, and holds no idea what
 * a battleship is: a socket joins the room named by its six-character code, and anything it
 * says is passed to whoever else is in that room. Both players still run the whole game
 * themselves, exactly as the two LAN peers do, and the same messages pass between them.
 */

/** One join code's worth of players. Lives only as long as somebody is connected. */
export class GameRoom {
  constructor(state) {
    this.state = state;
    this.sockets = new Set();
  }

  async fetch(request) {
    if (request.headers.get("Upgrade") !== "websocket") {
      return new Response("This endpoint speaks WebSocket.", { status: 426 });
    }

    const pair = new WebSocketPair();
    const client = pair[0];
    const server = pair[1];
    server.accept();
    this.sockets.add(server);

    server.addEventListener("message", (event) => {
      for (const peer of this.sockets) {
        if (peer === server || peer.readyState !== 1 /* OPEN */) {
          continue;
        }
        try {
          peer.send(event.data);
        } catch {
          this.sockets.delete(peer);
        }
      }
    });

    const drop = () => this.sockets.delete(server);
    server.addEventListener("close", drop);
    server.addEventListener("error", drop);

    return new Response(null, { status: 101, webSocket: client });
  }
}

/* The same alphabet the game generates codes from: no 0 or O, no 1 or I. */
const CODE = /^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}$/;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    // How the page works out that it has a relay to use. Anywhere else this 404s, which is
    // the answer as well, and the page quietly stops offering network play.
    if (url.pathname === "/ws-check") {
      return new Response("ok", { headers: { "cache-control": "no-store" } });
    }

    if (url.pathname === "/ws") {
      const code = (url.searchParams.get("code") || "").toUpperCase();
      if (!CODE.test(code)) {
        return new Response("That is not a join code.", { status: 400 });
      }
      const room = env.GAME_ROOM.get(env.GAME_ROOM.idFromName(code));
      return room.fetch(request);
    }

    // Assets are normally served before this script runs; this catches anything that slips past.
    return env.ASSETS.fetch(request);
  }
};
