package arbor.astralis.qotd;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public final class BackgroundQuestionDispatcher {
    
    private static volatile BackgroundQuestionDispatcher INSTANCE = null; 
    
    private BackgroundQuestionDispatcher(GatewayDiscordClient client) {
        var backgroundTimer = new Timer("question-dispatcher");

        long msUntilTomorrow = getMsUntilNextDayUtc();
        long msUntilTomorrowMins = TimeUnit.MILLISECONDS.toMinutes(msUntilTomorrow);
        Main.LOGGER.info("Background task scheduled with delay: " + msUntilTomorrow + "ms (" + msUntilTomorrowMins + " mins)");
        
        backgroundTimer.scheduleAtFixedRate(
            new QuestionDispatcherTask(client), 
            msUntilTomorrow,
            TimeUnit.DAYS.toMillis(1)
        );
    }

    public static void initialize(GatewayDiscordClient client) {
        if (INSTANCE == null) {
            synchronized (BackgroundQuestionDispatcher.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BackgroundQuestionDispatcher(client);
                }
            }
        }
    }

    private long getMsUntilNextDayUtc() {
        long msNow = System.currentTimeMillis();
        long msElapsedSinceToday = msNow % TimeUnit.DAYS.toMillis(1);
        long msTomorrow = msNow - msElapsedSinceToday + TimeUnit.DAYS.toMillis(1);
        
        return msTomorrow - msNow;
    }

    private static final class QuestionDispatcherTask extends TimerTask {
        
        private final GatewayDiscordClient client;
        
        private QuestionDispatcherTask(GatewayDiscordClient client) {
            this.client = client;
        }
        
        @Override
        public void run() {
            client.getGuilds()
                .doOnEach(guildDataSignal -> {
                    Guild guild = guildDataSignal.get();
                    
                    if (guild == null) {
                        return;
                    }
                    
                    QOTD.postRandomApprovedQuestion(client, guild.getId().asLong()).subscribe();
                })
                .subscribe();
        }
    }
}
