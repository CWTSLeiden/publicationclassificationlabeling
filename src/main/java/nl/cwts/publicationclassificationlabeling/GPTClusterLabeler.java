package nl.cwts.publicationclassificationlabeling;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.utils.TikTokensUtil;

/**
 * Cluster labeler based on OpenAI's GPT API.
 * 
 * @author Nees Jan van Eck
 */
public class GPTClusterLabeler
{
    /**
     * OpenAI API timeout parameters.
     */
    private static final int OPENAI_API_TIMEOUT = 45;
    private static final int OPENAI_API_TIMEOUT_SLEEP = 30;

    /**
     * OpenAI GPT model parameters.
     */
    private static final double TEMPARATURE = 0.3;
    private static final double TOP_P = 1.0;
    private static final double FREQUENCY_PENALTY = 0.3;
    private static final double PRESENCE_PENALTY = 0.3;
    private static final int MAX_TOKENS_COMPLETION = 320;

    /**
     * OpenAI GPT system prompt message.
     */
    private static final String SYSTEM_TASK_MESSAGE =
        "You will be provided with the titles of a representative sample of papers from a larger cluster of related scientific papers.\n\n"
        + "Your task is to identify the topic of the entire cluster based on the titles of the representative papers.\n\n"
        + "Output the following items (in English) that describe the topic of the cluster: 'short label' (at most 3 words and format in Title Case), 'long label' (at most 8 words and format in Title Case), list of 10 'keywords' (ordered by relevance and format in Title Case), 'summary' (few sentences), and 'wikipedia page' (URL).\n"
        + "Do not start short and long labels with the word \"The\".\n"
        + "Start each summary with \"This cluster of papers\".\n"
        + "Format the output in JSON.";

    /**
     * JSON code block pattern.
     */
    private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("(?s)^(```json)(.*)(```)$");

    /**
     * OpenAI API wrapper.
     */
    private static OpenAiService OpenAIService;

    /**
     * OpenAI GPT model.
     */
    private GPTModel model;

    /**
     * Constructs a GPT cluster labeler.
     * 
     * @param apiKey OpenAI API key
     * @param model  OpenAI GPT model
     */
    public GPTClusterLabeler(String apiKey, GPTModel model)
    {
        OpenAIService = new OpenAiService(apiKey, Duration.ofSeconds(OPENAI_API_TIMEOUT));
        this.model = model;
    }

    /**
     * Returns the labeling of a cluster based on the titles of the publications assigned to the cluster.
     * 
     * @param pubTitles Publication titles
     * 
     * @return Cluster labeling
     */
    public ClusterLabeling getClusterLabeling(String pubTitles)
    {
        // Clean publication titles and use them as user message.
        pubTitles = pubTitles.replaceAll("<[^>]*>", "");

        // Create prompt (system and user) messages.
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", SYSTEM_TASK_MESSAGE));
        messages.add(new ChatMessage("user", pubTitles));

        // Shorten user message if it is too long.
        boolean shortenedUserMessage = false;
        int tokensMessages = TikTokensUtil.tokens(model.type, messages);
        int maxTokensPrompt = model.maxTokens - MAX_TOKENS_COMPLETION;
        while (tokensMessages > maxTokensPrompt)
        {
            pubTitles = pubTitles.substring(0, pubTitles.length() - 400);
            shortenedUserMessage = true;
            messages = new ArrayList<>();
            messages.add(new ChatMessage("system", SYSTEM_TASK_MESSAGE));
            messages.add(new ChatMessage("user", pubTitles));
            tokensMessages = TikTokensUtil.tokens(model.type, messages);
        }
        if (shortenedUserMessage)
            System.out.print("Publication titles too long... ");

        // Use OpenAI's GPT API to generate a labeling.
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                .builder()
                .model(model.name)
                .temperature(TEMPARATURE)
                .maxTokens(MAX_TOKENS_COMPLETION)
                .topP(TOP_P)
                .frequencyPenalty(FREQUENCY_PENALTY)
                .presencePenalty(PRESENCE_PENALTY)
                .messages(messages)
                .build();
        StringBuilder builder = new StringBuilder();
        ChatCompletionResult chatCompletionResult = null;
        try
        {
            chatCompletionResult = OpenAIService.createChatCompletion(chatCompletionRequest);
        }
        catch (Exception e)
        {
            System.out.println(e.getClass());
            System.out.println(e.getCause());
            System.out.println(e.getMessage());
            if (e instanceof RuntimeException && e.getMessage().equals("java.net.SocketTimeoutException: timeout"))
            {
                try
                {
                    Thread.sleep(OPENAI_API_TIMEOUT_SLEEP * 1000);
                }
                catch (InterruptedException ie)
                {
                    throw new RuntimeException("Unexpected interrupt", ie);
                }
                return getClusterLabeling(pubTitles);
            }
            else
            {
                e.printStackTrace();
                try
                {
                    Thread.sleep(OPENAI_API_TIMEOUT_SLEEP * 1000);
                }
                catch (InterruptedException ie)
                {
                    throw new RuntimeException("Unexpected interrupt", ie);
                }
                return getClusterLabeling(pubTitles);
            }
        }
        chatCompletionResult.getChoices().forEach(choice ->
        {
            builder.append(choice.getMessage().getContent());
        });
        String response = builder.toString();

        // Parse API response.
        ClusterLabeling clusterLabeling = null;
        try
        {
            Matcher matcher = JSON_CODE_BLOCK_PATTERN.matcher(response);
            if (matcher.find())
                response = matcher.group(2);
            JSONObject jsonResponse = new JSONObject(response);
            String shortLabel = jsonResponse.optString("short label");
            if (shortLabel.isEmpty())
                shortLabel = jsonResponse.optString("short_label");
            String longLabel = jsonResponse.optString("long label");
            if (longLabel.isEmpty())
                longLabel = jsonResponse.optString("long_label");
            String summary = jsonResponse.optString("summary");
            JSONArray keywordsJSONArray = jsonResponse.optJSONArray("keywords");
            ArrayList<String> keywords = new ArrayList<String>();
            if (keywordsJSONArray != null)
                for(int i = 0; i < keywordsJSONArray.length(); i++)
                    keywords.add(keywordsJSONArray.getString(i));
            String wikipediaPage = jsonResponse.optString("wikipedia page");
            if (wikipediaPage.isEmpty())
                wikipediaPage = jsonResponse.optString("wikipedia_page");
            clusterLabeling = new ClusterLabeling(shortLabel, longLabel, keywords, summary, wikipediaPage);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(response);
            try
            {
                Thread.sleep(OPENAI_API_TIMEOUT_SLEEP * 1000);
            }
            catch (InterruptedException ie)
            {
                throw new RuntimeException("Unexpected interrupt", ie);
            }
            return getClusterLabeling(pubTitles);
        }

        return clusterLabeling;
    }
}
