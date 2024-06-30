package arbor.astralis.qotd;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public final class BackgroundQuestionDispatcher {

    private static final Logger LOGGER = LogManager.getLogger();
    private static volatile BackgroundQuestionDispatcher INSTANCE = null;
    
    private final GatewayDiscordClient client;
    private Timer timer;
    
    private BackgroundQuestionDispatcher(GatewayDiscordClient client) {
        this.client = client;
        respawnTimer();
    }

    private void respawnTimer() {
        if (timer != null) {
            timer.cancel();
        }
        
        timer = new Timer("question-dispatcher");
        
        long msUntilTomorrow = getMsUntilNextDayUtc();
        long msUntilTomorrowMins = TimeUnit.MILLISECONDS.toMinutes(msUntilTomorrow);
        LOGGER.info("New QOTD timer scheduled with delay: " + msUntilTomorrow + "ms (" + msUntilTomorrowMins + " mins)");
        
        timer.scheduleAtFixedRate(
            new QuestionDispatcherTask(client, this::respawnTimer),
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
        private final Runnable respawnTimerTask;

        private QuestionDispatcherTask(GatewayDiscordClient client, Runnable respawnTimerTask) {
            this.client = client;
            this.respawnTimerTask = respawnTimerTask;
        }
        
        @Override
        public void run() {
            client.getGuilds()
                .doOnEach(guildDataSignal -> {
                    Guild guild = guildDataSignal.get();
                    
                    if (guild == null) {
                        return;
                    }
                    
                    long guildId = guild.getId().asLong();
                    
                    QOTD.postRandomApprovedQuestion(client, guildId).subscribe();
                })
                .subscribe();
            
            respawnTimerTask.run();
        }
    }
}
