package fr;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Random;


@ApplicationScoped
public class ProducerFacturation implements Runnable {


	@Inject
	ConnectionFactory connectionFactory;

	@ConfigProperty(name = "quarkus.artemis.username")
	String userName;

	//indique si la classe est configurée pour recevoir les messages en boucle
	boolean running;
	Random random = new Random();

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

				Queue requestQueue = context.createQueue("M1.facturation-"+userName);
				Queue replyQueue = context.createQueue("M1.reply-facturation"+userName);

				TextMessage requestMessage;
				requestMessage = context.createTextMessage("Facture n° " + random.nextInt(100) + 1);
				requestMessage.setJMSReplyTo(replyQueue);

				context.createProducer().send(requestQueue, requestMessage);

			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}
}