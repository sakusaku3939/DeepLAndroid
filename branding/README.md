# App branding

- `app-icon.svg`: centered speech symbol without the app name.
- `header-logo.svg`: approved concept 01C, with a wider symbol-to-name gap for the app header.

The header SVG contains outlined text and needs no font at runtime. Android uses generated
VectorDrawables; the header and splash logo share a theme-aware foreground color.
The launcher foreground uses a centered 47.25 x 54 dp symbol on a 108 x 108 dp
layer, within Android's adaptive icon safe zone. Store artwork uses the symbol
at 74% of the image height.
The PNG/WebP resources cover legacy launchers and store listings.

To regenerate from the repository root:

```sh
python -m pip install -r branding/requirements.txt
python branding/generate_assets.py
```

The generator updates the vector resources, all launcher densities, the Play Store
icon, the Fastlane icon, and the light SVGs used by the WebView dark-theme patch. It does not modify the design proposal HTML.
