package fr;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ConsumerFacturation implements Runnable {

	@Inject
	ConnectionFactory connectionFactory;

	@ConfigProperty(name = "quarkus.artemis.username")
	String userName;

	boolean running;

	void onStart(@Observes StartupEvent ev) {
		running = true;
		new Thread(this).start();
	}


	void onStop(@Observes ShutdownEvent ev) {
		running = false;
	}

	@Override
	public void run() {

		try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {

			// creation de la queue
			Queue requestQueue = context.createQueue("M1.facturation-" + userName);

			while (running) {

				Message reqMess = context.createConsumer(requestQueue).receive();

				if (reqMess instanceof TextMessage) {
					String requestText = null;
					String replyTo = null;

					try {
						requestText = ((TextMessage) reqMess).getText();
						// Extraire l'adresse de réponse de l'en-tête du message
						 replyTo = reqMess.getJMSReplyTo().toString();

						 // On envoie un message sur l'addresse de retour
						 context.createProducer().send(reqMess.getJMSReplyTo(), requestText);

					} catch (JMSException e) {
						throw new RuntimeException(e);
					}
					System.out.println("Message reçu de la file de demande : " + requestText);
					System.out.println("Adresse de réponse : " + replyTo);
				}
			}
		}
	}
}
