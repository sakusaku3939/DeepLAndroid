$('[class^="TranslatorPage-module--otherSections--"]').hide();
$('a').css('pointer-events', 'none');
$('*').css('-webkit-tap-highlight-color', 'rgba(0,0,0,.1)');
$('footer').hide();

$('header').bind('DOMNodeInserted', () => {
    $('[class^="menuButton-module--menuButton--"]').hide();
    $('[class^="productNavigation-module--container--"]').children().not('[class^="productNavigation-module--translatorLink--"]').hide();
});