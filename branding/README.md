# App branding

- `app-icon.svg`: approved concept 01A, with the final vertical spacing.
- `header-logo.svg`: approved concept 01C, with the original horizontal spacing.

Both SVGs contain outlined text and need no font at runtime. Android uses generated
VectorDrawables; the header and splash logo share a theme-aware foreground color.
The launcher foreground is inset to keep the complete name inside adaptive masks.
The PNG/WebP resources cover legacy launchers and store listings.

To regenerate from the repository root:

```sh
python -m pip install -r branding/requirements.txt
python branding/generate_assets.py
```

The generator updates the vector resources, all launcher densities, the Play Store
icon, and the Fastlane icon. It does not modify the design proposal HTML.
