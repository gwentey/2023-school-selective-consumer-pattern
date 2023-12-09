package fr;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ConsumerServicePetitPret implements Runnable {

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
		while (running) {
			try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {
				String selector = "type = 'Petit Prêt'";

				JMSConsumer consumer = context.createConsumer(context.createQueue("M1.serviceDePret-" + userName), selector);
				Message messPetitPret = consumer.receive();

				String petitPretBody = messPetitPret.getBody(String.class);

				System.out.println("Réception d'un petit prêt : " + petitPretBody);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
