# Security

## Reporting a problem

Please don't describe a security problem in a public issue. Open an issue titled "Security contact" with no details, and I'll reply with a private way to send them.

Never paste an API key into an issue, a pull request, or a screenshot. If you did, delete the key at your provider and create a new one.

## What the app does with your data

- Your API key is encrypted on the phone with a key held in the Android Keystore. The app sends it only to the provider you picked, in the request headers that provider requires.
- What you say is turned into text on the phone by Android's speech recognizer. That text, the conversation so far, and the tutor prompt go to your provider for each reply. A translation sends that one message.
- Sessions and token totals are stored only on the phone. There's no backend, account, or analytics.

## Supported versions

Only the latest release gets fixes.
