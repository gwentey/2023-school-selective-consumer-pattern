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
public class ProducerPret implements Runnable {

	@Inject
	ConnectionFactory connectionFactory;

	@ConfigProperty(name = "quarkus.artemis.username")
	String userName;

	boolean running;
	Random random = new Random();

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

				Queue servicePret = context.createQueue("M1.serviceDePret-" + userName);

				Integer loanAmount = random.nextInt(200001);

				// Attribution de la valeur de sélection en fonction du montant du prêt
				String loanSelection = loanAmount < 100000 ? "Petit Prêt" : "Gros prêt";
				Integer numPret = random.nextInt(1000) + 1;

				// Création du message avec la valeur de sélection
				TextMessage requestMessage = context.createTextMessage("Pret n° " + numPret + " | Montant : " + loanAmount);
				System.out.println("* " + loanSelection + " crée * Pret n° " + numPret + " | Montant : " + loanAmount);

				requestMessage.setStringProperty("type", loanSelection);

				// Envoi du message à la queue avec la valeur de sélection appropriée
				context.createProducer().send(servicePret, requestMessage);

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}