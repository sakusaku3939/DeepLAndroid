var copyButton = $('.Target--Dwohg');

if (copyButton.length > 1) {
    copyButton.last().off('click');
    copyButton.last().on('click', function () {
        const text = $('[class^="TextInput-module--textarea--"]').last().val();
        /* Discontinued because the copy button now works on WebView as well
        Android.copyClipboard(text);
        */
    });
}