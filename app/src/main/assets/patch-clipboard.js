var copyButton = $('.Target--Dwohg');

if (copyButton.length > 1) {
    copyButton.last().off('click');
    copyButton.last().on('click', function () {
        const text = $('[class^="TextInput-module--textarea--"]').last().val();
        Android.copyClipboard(text);
    });
}