document.body.addEventListener('click', (e) => {
    console.log("class: " + e.target.className);
    console.log("id: " + e.target.id);
    console.log("html: " + e.target.outerHTML);
});

$('.pageHeader-module--inner--hlaCw').bind('DOMNodeInserted', (e) => {
    $('.menuButton-module--menuButton--3-IXD').hide();
    $('.productNavigation-module--container--2BnCV').children().not('.productNavigation-module--translatorLink--2_7y2').hide();
    $('a').css('pointer-events', 'none');
});

$('.TargetLanguageToolbar-module--container--3HO6W').bind('DOMNodeInserted', (e) => {
    $('.IconButton-module--button--1W1gd').hide();
});

$('.TranslatorPage-module--otherSections--3cbQ7').hide();
$('footer').hide();

/* $('button').css('-webkit-tap-highlight-color','rgba(0, 0, 0, 0)');
$('#dl_translator').siblings().hide();
$('.dl_header_menu_v2__buttons__menu').hide();
$('.dl_header_menu_v2__buttons__item').hide();
$('.dl_header_menu_v2__links').children().not('#dl_menu_translator_simplified').hide();
$('.dl_header_menu_v2__separator').hide();
$('.lmt__bottom_text--mobile').hide();
$('#dl_cookieBanner').hide();
$('.lmt__language_container_sec').hide();
$('.docTrans_translator_upload_button__inner_button').hide();
$('.lmt__target_toolbar__save').hide();
$('footer').hide();
$('a').css('pointer-events','none');
$('.lmt__sides_container').css('margin-bottom','32px'); */
