package audio;

import settings.Settings;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AudioManager {
    private enum SoundEffect {
        FIRE("/audio/fire.mp3"),
        EXPLOSION("/audio/explosion.mp3"),
        SELECT("/audio/select.mp3"),
        SIR_YES_SIR("/audio/siryessir.mp3"),
        INTERNET("/audio/internet.mp3"),
        BALLISTIC("/audio/ballistic.mp3");

        private final String resourcePath;

        SoundEffect(String resourcePath) {
            this.resourcePath = resourcePath;
        }
    }

    private final Map<SoundEffect, Media> soundEffects = new EnumMap<>(SoundEffect.class);
    private Media menuMedia;
    private Media victoryMedia;
    private Media lossMedia;
    private Media partyMedia;
    private MediaPlayer menuPlayer;
    private MediaPlayer endPlayer;
    private MediaPlayer partyPlayer;
    private boolean menuMusicInterruptedByParty;
    private MediaPlayer firePlayer;
    private MediaPlayer explosionPlayer;
    private MediaPlayer selectPlayer;
    private MediaPlayer sirYesSirPlayer;
    private MediaPlayer internetPlayer;
    private MediaPlayer ballisticPlayer;
    // The level each kind of sound is mixed at before the player's sliders are applied.
    private static final double MENU_MUSIC_LEVEL = 0.45;
    private static final double END_MUSIC_LEVEL = 0.65;
    private static final double EFFECT_LEVEL = 0.8;
    private static final double PARTY_LEVEL = 0.9;

    private boolean audioAvailable = true;

    public AudioManager() {
        for (SoundEffect soundEffect : SoundEffect.values()) {
            soundEffects.put(soundEffect, loadMedia(soundEffect.resourcePath));
        }
        menuMedia = loadMedia("/audio/menu.mp3");
        victoryMedia = loadMedia("/music/victory.mp3");
        lossMedia = loadMedia("/music/loss.mp3");
        partyMedia = loadMedia("/music/everybodydotheflop.mp3");
    }

    public void playFire() {
        firePlayer = playEffect(soundEffects.get(SoundEffect.FIRE), firePlayer, true);
    }

    public void playExplosion() {
        explosionPlayer = playEffect(soundEffects.get(SoundEffect.EXPLOSION), explosionPlayer, false);
    }

    public void playSelect() {
        if (sirYesSirPlayer != null && sirYesSirPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            return;
        }
        selectPlayer = playEffect(soundEffects.get(SoundEffect.SELECT), selectPlayer, false);
    }

    public void playSirYesSir() {
        sirYesSirPlayer = playEffect(soundEffects.get(SoundEffect.SIR_YES_SIR), sirYesSirPlayer, false);
    }

    public void playInternet() {
        internetPlayer = playEffect(soundEffects.get(SoundEffect.INTERNET), internetPlayer, false);
    }

    public void playBallistic() {
        ballisticPlayer = playEffect(soundEffects.get(SoundEffect.BALLISTIC), ballisticPlayer, false);
    }

    public void playMenuMusic() {
        stopEndMusic();
        if (!audioAvailable || menuMedia == null) {
            return;
        }
        if (menuPlayer == null) {
            try {
                menuPlayer = new MediaPlayer(menuMedia);
                menuPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                menuPlayer.setVolume(Settings.get().musicLevel(MENU_MUSIC_LEVEL));
                menuPlayer.setOnEndOfMedia(() -> {
                    try {
                        menuPlayer.seek(Duration.ZERO);
                        menuPlayer.play();
                    } catch (RuntimeException ignored) {
                    }
                });
                menuPlayer.setOnError(() -> {
                    audioAvailable = false;
                    menuPlayer = null;
                });
            } catch (RuntimeException exception) {
                audioAvailable = false;
                menuPlayer = null;
                return;
            }
        }
        try {
            if (menuPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
                menuPlayer.play();
            }
        } catch (RuntimeException ignored) {
        }
    }

    public void stopMenuMusic() {
        if (menuPlayer != null) {
            try {
                menuPlayer.stop();
            } catch (RuntimeException ignored) {
            }
        }
    }

    public void playVictoryMusic() {
        playEndMusic(victoryMedia);
    }

    public void playLossMusic() {
        playEndMusic(lossMedia);
    }

    public void stopEndMusic() {
        if (endPlayer != null) {
            disposePlayer(endPlayer);
            endPlayer = null;
        }
    }

    public void stopFire() {
        disposePlayer(firePlayer);
        firePlayer = null;
    }

    /**
     * Throws the party: stops whatever music is going, plays the track, and calls back when it
     * ends so the lights can be taken down. Menu music, if it was playing, is put back afterwards.
     */
    public void playParty(Runnable onFinished) {
        stopParty();
        if (!audioAvailable || partyMedia == null) {
            return;
        }
        menuMusicInterruptedByParty = menuPlayer != null;
        stopMenuMusic();
        try {
            partyPlayer = new MediaPlayer(partyMedia);
            partyPlayer.setVolume(Settings.get().musicLevel(PARTY_LEVEL));
            partyPlayer.setOnEndOfMedia(() -> endParty(onFinished));
            partyPlayer.setOnError(() -> endParty(onFinished));
            partyPlayer.setOnReady(() -> {
                try {
                    partyPlayer.play();
                } catch (RuntimeException ignored) {
                }
            });
        } catch (RuntimeException ignored) {
            partyPlayer = null;
        }
    }

    public boolean isPartyPlaying() {
        return partyPlayer != null;
    }

    public void stopParty() {
        disposePlayer(partyPlayer);
        partyPlayer = null;
    }

    private void endParty(Runnable onFinished) {
        stopParty();
        if (menuMusicInterruptedByParty) {
            menuMusicInterruptedByParty = false;
            playMenuMusic();
        }
        if (onFinished != null) {
            onFinished.run();
        }
    }

    /** Re-applies the volume sliders to whatever is playing right now. */
    public void refreshVolumes() {
        Settings settings = Settings.get();
        if (menuPlayer != null) {
            menuPlayer.setVolume(settings.musicLevel(MENU_MUSIC_LEVEL));
        }
        if (endPlayer != null) {
            endPlayer.setVolume(settings.musicLevel(END_MUSIC_LEVEL));
        }
        if (partyPlayer != null) {
            partyPlayer.setVolume(settings.musicLevel(PARTY_LEVEL));
        }
    }

    public void dispose() {
        stopMenuMusic();
        stopEndMusic();
        stopParty();
        disposePlayer(firePlayer);
        disposePlayer(explosionPlayer);
        disposePlayer(selectPlayer);
        disposePlayer(sirYesSirPlayer);
        disposePlayer(internetPlayer);
        disposePlayer(ballisticPlayer);
        disposePlayer(menuPlayer);
    }

    private Media loadMedia(String resourcePath) {
        String relativePath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        List<Path> fallbacks = List.of(
            Path.of("resources").resolve(relativePath),
            Path.of("out").resolve(relativePath),
            Path.of(relativePath)
        );
        for (Path fallback : fallbacks) {
            try {
                File file = fallback.toFile();
                if (file.exists()) {
                    return new Media(file.toURI().toString());
                }
            } catch (RuntimeException exception) {
                audioAvailable = false;
            }
        }

        URL resource = getClass().getResource(resourcePath);
        try {
            if (resource != null) {
                return new Media(resource.toExternalForm());
            }
        } catch (RuntimeException exception) {
        }
        return null;
    }

    private MediaPlayer playEffect(Media media, MediaPlayer existingPlayer, boolean stopAfterOneSecond) {
        if (!audioAvailable || media == null) {
            return existingPlayer;
        }
        disposePlayer(existingPlayer);
        try {
            MediaPlayer player = new MediaPlayer(media);
            player.setVolume(Settings.get().effectsLevel(EFFECT_LEVEL));
            player.setOnEndOfMedia(player::dispose);
            player.setOnError(() -> {
                audioAvailable = false;
                player.dispose();
            });
            player.setOnReady(() -> {
                try {
                    if (stopAfterOneSecond) {
                        player.setStopTime(Duration.seconds(1));
                    }
                    player.play();
                } catch (RuntimeException ignored) {
                }
            });
            return player;
        } catch (RuntimeException exception) {
            audioAvailable = false;
            return existingPlayer;
        }
    }

    private void playEndMusic(Media media) {
        stopMenuMusic();
        stopEndMusic();
        if (!audioAvailable || media == null) {
            return;
        }
        try {
            endPlayer = new MediaPlayer(media);
            endPlayer.setVolume(Settings.get().musicLevel(END_MUSIC_LEVEL));
            endPlayer.setOnEndOfMedia(() -> {
                try {
                    endPlayer.stop();
                } catch (RuntimeException ignored) {
                }
            });
            endPlayer.setOnError(() -> {
                audioAvailable = false;
                disposePlayer(endPlayer);
                endPlayer = null;
            });
            endPlayer.setOnReady(() -> {
                try {
                    endPlayer.play();
                } catch (RuntimeException ignored) {
                }
            });
        } catch (RuntimeException exception) {
            audioAvailable = false;
            endPlayer = null;
        }
    }

    private void disposePlayer(MediaPlayer player) {
        if (player == null) {
            return;
        }
        try {
            player.stop();
        } catch (RuntimeException ignored) {
        }
        try {
            player.dispose();
        } catch (RuntimeException ignored) {
        }
    }
}
