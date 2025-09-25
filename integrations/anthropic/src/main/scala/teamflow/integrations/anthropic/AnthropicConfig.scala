package teamflow.integrations.anthropic

import eu.timepit.refined.types.string.NonEmptyString
import java.net.URI

case class AnthropicConfig(
    enabled: Boolean,
    apiUrl: URI,
    apiKey: NonEmptyString,
    defaultModel: NonEmptyString,
    defaultMaxTokens: Int
)