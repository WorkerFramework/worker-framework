## Localizing Documentation

Using the layouts provided we can support multiple different languages when translations are available.

When writing documentation there are a few patterns you should be aware of and follow regardless of whether your documentation is available in multiple languages or not.

#### Languages

The various supported languages are found in the Documentation repository in the `_data/localizations` folder in the `languages.json` file.

This file contains a list of available languages (as abbreviations such as en-us etc.) and their corresponding country, continent and language name. The abbreviation is used to identify each language, and therefore should be used when referring to a language.

An example of the layout of `languages.json` is as follows:

```json
{
    "en-us": {
      "country": "United States",
      "language": "English",
      "continent": "North America"
    },
    "es": {
      "country": "España",
      "language": "Español",
      "continent": "Europe"
    },
    ...
}
```

To allow support for additional languages they should be added to this file. The default language is `en-us`, so in the case of an item that does not have support for a particular language it will display the `en-us` version.

#### Documentation Folder Structure

Each repository should have a `docs` folder that will contain all the markdown files. This folder however should have sub folders for each different language.

These folders should be named matching the language abbreviations specified in the `languages.json` file.

An example folder structure could be:

```
docs/
  |--- en-us/
    |--- getting_started.md
    |--- overview.md
    |--- api.md
  |--- es/
    |--- empezando.md
    |--- visión_de_conjunto.md
    |--- api.md
```

Even if only one language is initially available, structuring folders in this way will allow more languages to be added over time.

#### Localizing the Footer, Top & Side Navigation

In order to localize any link text in the `footer_links.json` (see [guide to navigation](navigation.md)) we need to ensure the JSON file is structured accordingly.

All items in the footer are contained within a `navigation_items` array where each item will have a `title` and a `url` object. These objects will have a key (the language abbreviation) and a value (the localized text or URL).

An example of the layout of `footer_links.json` is as follows:

```json
{
    "navigation_items": [
    {
        "title": {
            "en-us": "Contribute",
            "es": "Contribuir",
            "fr": "Contribuer"
        },
        "url": {
            "en-us": "pages/en-us/contribute",
            "es": "pages/es/contribute",
            "fr": "pages/fr/contribute"
        }
    },
    ...
    ]
}
```

Localizing the `top_navigation.json` is the same except we have the addition of an 2 optional variables, `icon` and `primary`, which are described in the [guide to navigation](navigation.md).

Localizing the `side_navigation.json` is also the same except we have the addition of 2 optional variables, 'icon' and 'children' which are also described in [guide to navigation](navigation.md).
The `children` array is the same as its parent in that it takes a `title` and a `url` object. These objects then work as described previously for `footer_links.json`.

An example of the layout of `side_navigation.json` is as follows:

```json
{
    "navigation_items": [
    {
        "title": {
            "en-us": "Overview",
            "es": "Visión de conjunto",
            "fr": "Aperçu"
        },
        "icon": "hpe-cloud",
        "children": [
        {
            "title": {
                "en-us": "What is CAF?",
                "es": "¿Cuál es la CAF ?",
                "fr": "Qu'est-ce que la CAF ?"
            },
            "url": {
                "en-us": "pages/en-us/what_is_caf",
                "es": "pages/es/what_is_caf",
                "fr": "pages/fr/what_is_caf"
            }
        }, {
            "title": {
                "en-us": "Architecture",
                "es": "Arquitectura",
                "fr": "Architecture"
            },
            "url": {
                "en-us": "pages/en-us/architecture",
                "es": "pages/es/architecture",
                "fr": "pages/fr/architecture"
            }
        }]
    }]
}
```

*Note: If you don't provide a translation for a URL or title, then the default (en-us) language will be displayed.*

#### Localizing Non-Navigation Item Pages

Each markdown page can have YAML frontmatter that specifies all the possible URLs that the page can have, contained within a `localized_urls` object. Each item will have the language abbreviation (same as `languages.json`) and the appropriate URL for that language. This ensures the correct page is navigated to when the language is changed.

This works for both navigation items (side navigation, footer etc.) if no JSON URL object is specified and for non-navigation items which do not have a JSON.

An example of the YAML frontmatter on our `what_is_caf.md` wouled be as follows:

```yaml
---
layout: default
title: What is CAF

localized_urls:
    en-us: pages/en-us/what_is_caf
    es: pages/es/what_is_caf
    fr: pages/fr/what_is_caf
---
```

*Note: The frontmatter needs to be consistent on every language specific markdown page, i.e. if the `localized_urls` frontmatter is on `pages/en-us/what_is_caf.md` then it must also be on `pages/es/what_is_caf.md`.*