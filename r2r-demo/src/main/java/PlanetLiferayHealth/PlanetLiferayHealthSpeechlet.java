/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package PlanetLiferayHealth;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;


public class PlanetLiferayHealthSpeechlet implements Speechlet {
    private static final Logger log = LoggerFactory.getLogger(PlanetLiferayHealthSpeechlet.class);

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        return getWelcomeResponse();
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;

        if ("PlanetLiferayHealth".equals(intentName)) {
	        try {
		        return getHealthStatusResponse();
	        }
	        catch (IOException e) {
		        throw new RuntimeException(
		        	"Cannot fetch health status from Liferay", e);
	        }
        } else if ("AMAZON.HelpIntent".equals(intentName)) {
            return getHelpResponse();
        } else {
            throw new SpeechletException("Invalid Intent");
        }
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        // any cleanup logic goes here
    }

    /**
     * Creates and returns a {@code SpeechletResponse} with a welcome message.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getWelcomeResponse() {
        String speechText = "Welcome to Liferay Skills Kit, you can ask health check";

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("HealthStatusIntent");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }

	/**
	 * Creates a {@code SpeechletResponse} for the hello intent.
	 *
	 * @return SpeechletResponse spoken and visual response for the given intent
	 */
	private SpeechletResponse getHealthStatusResponse() throws IOException{
		String speechText = "RESPONSE GOES HERE";

		UsernamePasswordCredentials creds = new UsernamePasswordCredentials("test", "R3m3mb3r112");

		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
			new AuthScope("cloud-10-0-20-51.liferay.com", 80),
			creds);
		CloseableHttpClient httpclient = HttpClients.custom()
			.setDefaultCredentialsProvider(credsProvider)
			.build();

		try {
			HttpGet httpGet = new HttpGet("https://cloud-10-0-20-51.liferay.com/c/portal/health");
			httpGet.addHeader(BasicScheme.authenticate(creds, "US-ASCII", false));
			CloseableHttpResponse response = httpclient.execute(httpGet);
			try {

				String body = EntityUtils.toString(response.getEntity());
				log.info("RESPONSE: " + body);
				String[] statusResults = body.split("\\|");

				String[] uptime = statusResults[0].split(",");

				log.info("UPTIME STRING: " + statusResults[0]);
				log.info("UPTIME: " + Arrays.toString(uptime));

				speechText = "The server has been up for " + uptime[0] + " days " + uptime[1] + " hours and " + uptime[2] + " minutes";

				String[] ramUsage = statusResults[1].split(",");

				String current = String.valueOf((int) ((Double.valueOf(ramUsage[0].trim())/Long.valueOf(ramUsage[1].trim()))*100));

				speechText = speechText + ", the memory usage is at " + current + " percent, ";

				String liveUsers = statusResults[2].trim();

				speechText = speechText + "and there are currently " + liveUsers + " users logged in.";
			} finally {
				response.close();
			}
		} finally {
			httpclient.close();
		}

		// Create the Simple card content.
		SimpleCard card = new SimpleCard();
		card.setTitle("HealthStatusIntent");
		card.setContent(speechText);

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);

		return SpeechletResponse.newTellResponse(speech, card);
	}

   /**
     * Creates a {@code SpeechletResponse} for the help intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelpResponse() {
        String speechText = "You can ask health check or health status";

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("HealthStatusIntent");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }

}
