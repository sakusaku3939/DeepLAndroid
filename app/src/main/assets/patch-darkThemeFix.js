document.querySelector('.dl_header_menu_v2__logo__img')
    ?.setAttribute('src', 'data:image/svg+xml;base64,' + Android.stringToBase64String(Android.getAssetsText('DeepL_Logo_lightBlue_v2.svg')));
document.querySelector('.dl_logo_text')
    ?.setAttribute('src', 'data:image/svg+xml;base64,' + Android.stringToBase64String(Android.getAssetsText('DeepL_Text_light.svg')));
