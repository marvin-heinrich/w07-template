Aufgabe 2: Containerisierung und Lokale Orchestrierung
Szenario: Die Mensa App besteht aus einer Spring Boot Server-Komponente und einer Svelte Client-Komponente. Sie sollen die Bereitstellung beider Anwendungen für die lokale Entwicklung und das Testen mittels Containern vereinfachen.
Ihre Aufgabe:
1. Erstellen oder modifizieren Sie die Dockerfiles für die Spring Boot Server-Anwendung und die Svelte Client-Anwendung [W04, Folie 140-141]. Verwenden Sie dabei Multi-Stage Builds, um die Image-Größe und Sicherheit zu optimieren [W04, Folie 142-143].
2. Erstellen Sie eine docker-compose.yml Datei, die beide Container-Services definiert [W04, Folie 148]:
    ◦ Definieren Sie, dass der Client-Service vom Server-Service abhängt (depends_on).
    ◦ Konfigurieren Sie die Netzwerkverbindung so, dass der Client den Server über seinen Dienstnamen (service name) erreichen kann [W04, Folie 151-152].
    ◦ Stellen Sie sicher, dass sowohl der Client als auch der Server über bestimmte Ports vom Host aus erreichbar sind [W04, Folie 152].
    ◦ Fügen Sie Health Checks für den Server-Service hinzu [W04, Folie 158].
3. Beschreiben Sie kurz, wie Sie diese docker-compose.yml verwenden würden, um die Anwendung zu starten, zu überprüfen, ob sie läuft, und wieder zu stoppen [W04, Folie 153-154].