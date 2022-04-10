if ($('.lmt').length) {
    /* Old design */
    $('.lmt__translations_as_text').on('DOMSubtreeModified propertychange', function () {
        const copyButton = $('.lmt__translations_as_text__copy_button, .lmt__target_toolbar__copy');
        copyButton.off('click');
        copyButton.on('click', function () {
            const text = $('.lmt__translations_as_text__text_btn').eq(0).text();
            Android.copyClipboard(text);
        });
    });
} else {
    /* New design */
    var copyButton = $('.IconButton-module--button--1W1gd');
    copyButton.off('click');
    copyButton.on('click', function () {
        const text = $(".TargetTextInput-module--textarea--1dPfl").val();
        Android.copyClipboard(text);
    });
}