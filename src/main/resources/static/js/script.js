document.getElementById('btn-get-recipe').addEventListener('click', function(event) {
    event.preventDefault(); // Forhindrer formularen i at sende og genindlæse siden
    const userInput = document.getElementById('recipe-name').value;

    if (userInput.trim() === "") {
        alert("Indtast venligst noget tekst");
        return;
    }

    // Vis spinneren
    document.getElementById('spinner').style.display = 'block';

    // Send forespørgsel til backend for at få en opskrift baseret på brugerens input
    fetch(`http://localhost:8080/api/recipes/generate?query=${encodeURIComponent(userInput)}`, {
        method: 'GET'
    })
        .then(response => response.json())
        .then(data => {
            console.log(data);
            // Tilføj AI's svar til resultatet
            document.getElementById('result').textContent = data.answer;
        })
        .catch(error => {
            console.error("Fejl:", error);
            document.getElementById('result').textContent = "Der opstod en fejl. Prøv venligst igen senere.";
        })
        .finally(() => {
            // Skjul spinneren
            document.getElementById('spinner').style.display = 'none';
        });

    // Ryd inputfeltet
    document.getElementById('recipe-name').value = '';
});
