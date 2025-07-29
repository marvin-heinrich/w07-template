Aufgabe 1: Erweiterung einer bestehenden Funktion und Git-Workflow
Szenario: Die Mensa App soll um die Möglichkeit erweitert werden, die täglichen Öffnungszeiten einer Mensa anzuzeigen. Bisher werden nur die Speisepläne angezeigt.
Ihre Aufgabe:
1. Erstellen Sie einen neuen Feature Branch in Ihrem Git-Repository, z.B. feature/mensa-oeffnungszeiten [W03, Folie 104].
2. Fügen Sie der Server-Logik (Spring Boot CanteenController und/oder CanteenService) eine neue API-Endpunkt-Methode hinzu, die die Öffnungszeiten für eine gegebene Mensa zurückgibt [W03, Folie 105, 269]. Sie können hierfür Dummy-Daten verwenden oder die eat-api erweitern, falls die Funktionalität dort existiert.
3. Erweitern Sie die Client-Anwendung (Svelte), um diese Öffnungszeiten von der neuen API abzurufen und prominent im UI der Mensa-Detailansicht anzuzeigen [W03, Folie 106-109].
4. Erstellen Sie einen grundlegenden Integrationstest für den neuen Server-Endpunkt, um sicherzustellen, dass er korrekte Daten zurückgibt und bei nicht gefundenen Mensen entsprechend reagiert (z.B. HttpStatus.NO_CONTENT) [W03, Folie 110-114].
5. Führen Sie alle lokalen Tests aus und stellen Sie sicher, dass sie erfolgreich sind [W03, Folie 115].
6. Committen und pushen Sie Ihre Änderungen auf den Feature Branch [W03, Folie 109]. Achten Sie dabei auf aussagekräftige Commit-Nachrichten [W03, Folie 99].
7. Erstellen Sie einen Pull Request von Ihrem Feature Branch in den main oder development Branch (falls vorhanden) und verlinken Sie diesen mit einem neuen oder bestehenden GitHub Issue, das die Aufgabe beschreibt [W03, Folie 116-119, 102-103].