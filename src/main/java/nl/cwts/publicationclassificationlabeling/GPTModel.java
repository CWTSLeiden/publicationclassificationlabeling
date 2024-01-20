package nl.cwts.publicationclassificationlabeling;

import java.util.HashMap;
import java.util.Map;

/**
 * OpenAI GPT models that can be used to label clusters.
 * 
 * @author Nees Jan van Eck
 */
public enum GPTModel
{
    GPT_4_1106_PREVIEW("gpt-4-1106-preview", "gpt-4", 128000),
    GPT_4("gpt-4", "gpt-4", 8192),
    GPT_4_32K("gpt-4-32k", "gpt-4", 32768),
    GPT_3_5_TURBO_1106("gpt-3.5-turbo-1106", "gpt-3.5-turbo", 16385),
    GPT_3_5_TURBO("gpt-3.5-turbo", "gpt-3.5-turbo", 4097),
    GPT_3_5_TURBO_16K("gpt-3.5-turbo-16k", "gpt-3.5-turbo", 16385);

    private static final Map<String, GPTModel> MODELS_BY_NAME = new HashMap<>();

    static
    {
        for (GPTModel e : values())
            MODELS_BY_NAME.put(e.name, e);
    }

    /**
     * GPT model name.
     */
    public final String name;

    /**
     * GPT model type.
     */
    public final String type;

    /**
     * GPT model context window.
     */
    public final int maxTokens;

    /**
     * Constructs a GPTModel enum instance.
     * 
     * @param name      GPT model name
     * @param type      GPT model type
     * @param maxTokens GPT model context window
     */
    private GPTModel(String name, String type, int maxTokens)
    {
        this.name = name;
        this.type = type;
        this.maxTokens = maxTokens;
    }

    /**
     * Returns an OpenAI GTP model by name.
     * 
     * @param name GPT model name
     * 
     * @return OpenAI GPT model
     */
    public static GPTModel getModel(String name)
    {
        return MODELS_BY_NAME.get(name);
    }
}
