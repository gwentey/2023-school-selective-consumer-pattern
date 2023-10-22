package fr;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class ReplyConsumer implements Runnable {

	@Inject
	ConnectionFactory connectionFactory;

	@ConfigProperty(name = "quarkus.artemis.username")
	String userName;

	//indique si la classe est configurée pour recevoir les messages en boucle
	boolean running;

	//cette méthode démarre un nouveau thread exécutant l'instance en cours, jusqu'à ce que la variable running soit false.
	void onStart(@Observes StartupEvent ev) {
		running = true;
		new Thread(this).start();
	}


	void onStop(@Observes ShutdownEvent ev) {
		running = false;
	}

	@Override
	public void run() {
		while (running) {

			try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {

				Queue replyQueue = context.createQueue("M1.reply-facturation" + userName);

				Message messNotification = context.createConsumer(replyQueue).receive();

				if (messNotification instanceof TextMessage) {
					String requestText = null;
					try {
						requestText = ((TextMessage) messNotification).getText();
						System.out.println("[NOTIFICATION BRANCHE REPLY]" + requestText);
					} catch (JMSException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

}
