document.body.addEventListener('click', (e) => {
    console.log("class: " + e.target.className);
    console.log("id: " + e.target.id);
    /*console.log("html: " + e.target.outerHTML);*/
});