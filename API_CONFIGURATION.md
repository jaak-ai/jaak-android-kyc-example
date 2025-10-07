# API Configuration

This project uses `local.properties` to configure the API base URL. This file is ignored by Git to prevent sensitive URLs from being committed to the repository.

## Setup

1. Copy the example file:
   ```bash
   cp local.properties.example local.properties
   ```

2. Edit `local.properties` and set your API base URL:
   ```properties
   api.base.url=https://api.qa.jaak.ai/
   ```

3. The URL will be automatically loaded into `BuildConfig.API_BASE_URL` during build time.

## Important Notes

- **DO NOT** commit `local.properties` to the repository
- The `local.properties` file is already in `.gitignore`
- Each developer should configure their own `local.properties` file
- Use `local.properties.example` as a template for new team members

## Troubleshooting

If you see an error like:
```
API_BASE_URL not configured. Please add 'api.base.url' to local.properties
```

Make sure you have created `local.properties` with the `api.base.url` property set.
