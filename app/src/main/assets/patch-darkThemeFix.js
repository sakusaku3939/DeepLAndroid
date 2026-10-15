document.querySelector('.dl_header_menu_v2__logo__img')
    ?.setAttribute('src', 'data:image/svg+xml;base64,' + Android.stringToBase64String(Android.getAssetsText('brand-symbol-light.svg')));
document.querySelector('.dl_logo_text')
    ?.setAttribute('src', 'data:image/svg+xml;base64,' + Android.stringToBase64String(Android.getAssetsText('brand-wordmark-light.svg')));
