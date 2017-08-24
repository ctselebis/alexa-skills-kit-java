/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package PlanetLiferayNewUser;

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


public class PlanetLiferayNewUserSpeechlet implements Speechlet {
    private static final Logger log = LoggerFactory.getLogger(PlanetLiferayNewUserSpeechlet.class);

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

        if ("PlanetLiferayNewUser".equals(intentName)) {
	        try {

		        Map<String, Slot> slots = intent.getSlots();

		        // Get the color slot from the list of slots.
		        Slot nameSlot = slots.get("Name");
		        return getAddNewUserResponse(nameSlot.getValue());
	        }
	        catch (IOException e) {
		        throw new RuntimeException(
			        "Cannot add new user to Liferay", e);
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
        String speechText = "Welcome to Liferay Skills Kit, you can say New User followed by the user's first and last name";

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("PlanetLiferayNewUserIntent");
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
	private SpeechletResponse getAddNewUserResponse(String userName) throws IOException{
		String speechText = "RESPONSE GOES HERE";

		UsernamePasswordCredentials creds = new UsernamePasswordCredentials("test", "R3m3mb3r112");

		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
			new AuthScope("cloud-10-0-20-51.liferay.com", 80),
			creds);
		CloseableHttpClient httpclient = HttpClients.custom()
			.setDefaultCredentialsProvider(credsProvider)
			.build();

		String[] names = userName.split(" ");
		String firstName = names[0].trim();
		String lastName = names[1].trim();
		log.info("FIRST NAME: " + firstName);
		log.info("LAST NAME: " + lastName);

		try {
			HttpGet httpGet = new HttpGet("https://cloud-10-0-20-51.liferay.com/api/jsonws/user/add-user/company-id/20116/auto-password/true/-password1/-password2/auto-screen-name/true/-screen-name/-email-address/facebook-id/0/-open-id/locale/new%20Locale(%22en_US%22)/first-name/"+firstName+"/-middle-name/last-name/"+lastName+"/prefix-id/0/suffix-id/0/male/true/birthday-month/1/birthday-day/1/birthday-year/1/-job-title/-group-ids/-organization-ids/-role-ids/-user-group-ids/send-email/false");
			httpGet.addHeader(BasicScheme.authenticate(creds, "US-ASCII", false));
			CloseableHttpResponse response = httpclient.execute(httpGet);
			try {
				log.info("NEW USER: " + userName);
				speechText = "The user " + userName + " was added.";
			}
			finally {
				response.close();
			}
		}
		catch (Exception e) {
			speechText = "I'm sorry, " + userName + " could not be added.";
		}
		finally {
			httpclient.close();
		}

		// Create the Simple card content.
		SimpleCard card = new SimpleCard();
		card.setTitle("PlanetLiferayNewUserIntent");
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
        String speechText = "You can say Planet Lifear New User followed by first and last name";

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("PlanetLiferayNewUserIntent");
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
