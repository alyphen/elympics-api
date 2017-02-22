# elympics-api

This is a Java API for Dollarone's elympics.
See https://dollarone.games/elympics/ to get a key.

## Maven
```xml
<repositories>
    <repository>
        <id>seventh-root-repo</id>
        <url>https://repo.seventh-root.com/artifactory/libs-release-local/</url>
    </repository>
</repositories>
<dependencies>
    <dependency>
        <groupId>games.dollarone</groupId>
        <artifactId>elympics-api</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

## Gradle
```groovy
repositories {
    maven { url "https://repo.seventh-root.com/artifactory/libs-release-local" }
}
dependencies {
    compile group: 'games.dollarone', name: 'elympics-api', version: '1.0.0'
}
```

## Usage
```java
Elympics elympics = Elympics.connect("xxx"); // Where xxx is your API key

// Viewing high scores in order
try {
    final List<ElympicsHighscore> highscores = elympics.getHighscores();
    Collections.sort(highscores);
    for (ElympicsHighscore highscore : highscores) {
        System.out.println(highscore.getName() + " - " + highscore.getScore());
    }
} catch (IOException exception) {
    // Handle failure
}

// Viewing high scores in reverse order
try {
    final List<ElympicsHighscore> highscores = elympics.getHighscores();
    highscores.sort(Collections.reverseOrder());
} catch (IOException exception) {
    // Handle failure
}

// Submitting a high score
try {
    // With an object with a BigInteger score
    elympics.submitHighscore(new ElympicsHighscore("Name", BigInteger.valueOf(9001)));
    
    // With an object with a primitive score (long, int, etc)
    elympics.submitHighscore(new ElympicsHighscore("Name", 9001));
    
    // No object, BigInteger score
    elympics.submitHighscore("Name", BigInteger.valueOf(9001));
    
    // No object, primitive score (long, int, etc)
    elympics.submitHighscore("Name", 9001);
} catch (IOException exception) {
    // Handle failure
}
```

This work includes modified parts of the GitHub API for Java, Copyright (c) 2011 Kohsuke Kawaguchi and other contributors, licensed under the MIT license. The license may be found at LICENSE-github-api-for-java.
