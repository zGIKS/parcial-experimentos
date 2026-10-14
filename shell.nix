{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  packages = with pkgs; [
    jdk25
    maven
    plantuml
  ];

  shellHook = ''
    export JAVA_HOME=${pkgs.jdk25}
    export MAVEN_OPTS="-Xmx1g"
    echo "EntregaGo: Java $(java -version 2>&1 | head -n 1), Maven $(mvn -version | head -n 1)"
    echo "Ejecutar API: mvn spring-boot:run | Ejecutar pruebas: mvn test"
    echo "Renderizar UML: plantuml -tsvg diagrams/*.puml"
  '';
}
