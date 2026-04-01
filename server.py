#!/usr/bin/env python3
"""
Battleship LAN Server  —  pure Python standard library, no extra packages needed.

HOW TO USE:
  1. Run this script:  python server.py
  2. It prints a URL like  http://192.168.1.x:50505
  3. BOTH players open that URL in their browser (same Wi-Fi / LAN)
  4. One clicks "LAN Multiplayer" → Host, the other clicks Join and types the room code.

Press Ctrl+C to stop the server.
"""
import hashlib, base64, struct, json, os, mimetypes, urllib.parse, socket, random, threading
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler

PORT    = 50505
BASEDIR = os.path.dirname(os.path.abspath(__file__))

rooms      = {}          # code -> {'host': sock, 'guest': sock | None}
rooms_lock = threading.Lock()

# ─── WebSocket helpers ────────────────────────────────────────────────────────

def _ws_accept(key):
    magic = '258EAFA5-E914-47DA-95CA-C5AB0DC85B11'
    digest = hashlib.sha1((key.strip() + magic).encode()).digest()
    return base64.b64encode(digest).decode()

def ws_recv(sock):
    """Read one WebSocket frame; return decoded text or None on close/error."""
    try:
        def read(n):
            buf = b''
            while len(buf) < n:
                chunk = sock.recv(n - len(buf))
                if not chunk:
                    raise ConnectionError
                buf += chunk
            return buf

        b1, b2 = read(2)
        opcode  = b1 & 0x0F
        if opcode == 8:
            return None                      # close frame
        masked  = b2 >> 7
        length  = b2 & 0x7F
        if length == 126:
            length = struct.unpack('>H', read(2))[0]
        elif length == 127:
            length = struct.unpack('>Q', read(8))[0]
        mask    = read(4) if masked else b''
        payload = read(length)
        if masked:
            payload = bytes(b ^ mask[i % 4] for i, b in enumerate(payload))
        return payload.decode('utf-8', errors='replace')
    except Exception:
        return None

def ws_send(sock, obj):
    """Send a JSON object as a WebSocket text frame."""
    try:
        data = json.dumps(obj).encode('utf-8')
        n    = len(data)
        if   n <= 125:    sock.sendall(bytes([0x81, n]) + data)
        elif n < 65536:   sock.sendall(struct.pack('>BBH', 0x81, 126, n) + data)
        else:             sock.sendall(struct.pack('>BBQ', 0x81, 127, n) + data)
    except Exception:
        pass

# ─── Room / relay logic ───────────────────────────────────────────────────────

def _new_code():
    chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
    return ''.join(random.choices(chars, k=6))

def _lan_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return '127.0.0.1'

def ws_session(sock):
    role = code = None
    try:
        while True:
            raw = ws_recv(sock)
            if raw is None:
                break
            try:
                msg = json.loads(raw)
            except Exception:
                continue
            t = msg.get('type')

            if t == 'HOST':
                code = _new_code()
                role = 'host'
                with rooms_lock:
                    rooms[code] = {'host': sock, 'guest': None}
                ws_send(sock, {'type': 'HOSTED', 'code': code, 'ip': _lan_ip()})

            elif t == 'JOIN':
                join_code = str(msg.get('code', '')).strip().upper()
                with rooms_lock:
                    room      = rooms.get(join_code)
                    available = room is not None and room['guest'] is None
                    if available:
                        room['guest'] = sock
                        code          = join_code
                        role          = 'guest'
                        host_sock     = room['host']
                    else:
                        host_sock = None
                if host_sock:
                    ws_send(sock,      {'type': 'JOINED'})
                    ws_send(host_sock, {'type': 'GUEST_JOINED'})
                else:
                    ws_send(sock, {'type': 'ERROR', 'msg': 'Room not found or already full.'})

            else:
                # Relay every other message to the partner
                if code and role:
                    with rooms_lock:
                        room    = rooms.get(code, {})
                        partner = room.get('guest' if role == 'host' else 'host')
                    if partner:
                        ws_send(partner, msg)

    finally:
        if code:
            with rooms_lock:
                room    = rooms.get(code, {})
                partner = room.get('guest' if role == 'host' else 'host')
                if role == 'host':
                    rooms.pop(code, None)
                elif code in rooms:
                    rooms[code]['guest'] = None
            if partner:
                ws_send(partner, {'type': 'DISCONNECT'})
        try:
            sock.close()
        except Exception:
            pass

# ─── HTTP handler ─────────────────────────────────────────────────────────────

class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass   # keep console clean

    def do_GET(self):
        # ── WebSocket upgrade ──────────────────────────────────────────────
        if self.headers.get('Upgrade', '').lower() == 'websocket':
            key    = self.headers.get('Sec-WebSocket-Key', '')
            accept = _ws_accept(key)
            self.wfile.write(
                f'HTTP/1.1 101 Switching Protocols\r\n'
                f'Upgrade: websocket\r\nConnection: Upgrade\r\n'
                f'Sec-WebSocket-Accept: {accept}\r\n\r\n'
                .encode()
            )
            self.wfile.flush()
            # Block until this WebSocket session ends (threading server = own thread)
            ws_session(self.connection)
            self.close_connection = True
            return

        # ── Serve static files ─────────────────────────────────────────────
        path  = urllib.parse.unquote(self.path.split('?')[0])
        if path == '/':
            path = '/battleship.html'
        local = os.path.normpath(os.path.join(BASEDIR, path.lstrip('/')))
        if not local.startswith(BASEDIR) or not os.path.isfile(local):
            self.send_error(404)
            return
        mime, _ = mimetypes.guess_type(local)
        self.send_response(200)
        self.send_header('Content-Type', mime or 'application/octet-stream')
        self.send_header('Cache-Control', 'no-cache')
        self.end_headers()
        with open(local, 'rb') as f:
            self.wfile.write(f.read())

# ─── Entry point ──────────────────────────────────────────────────────────────

if __name__ == '__main__':
    lan_ip = _lan_ip()
    srv    = ThreadingHTTPServer(('', PORT), Handler)
    print(f'\n  Battleship LAN Server is running.\n')
    print(f'  Open this URL on BOTH computers (same Wi-Fi/network):')
    print(f'  --> http://{lan_ip}:{PORT}\n')
    print(f'  Press Ctrl+C to stop.\n')
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        print('\nServer stopped.')
