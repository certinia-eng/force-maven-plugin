* Clone repo
* mvn install
* Create maven  profile in settings.xml

        <profile>
        <id>my-project</id>
        <properties>
            <sf.username>xxx</sf.username>
            <sf.password>xxx</sf.password>
            <sf.deployRoot>/Users/myuser/myprojectproject/src</sf.deployRoot>
        </properties>
        </profile>
        <activeProfiles>
            <activeProfile>my-project</activeProfile>
        </activeProfiles>

* cd to my-project
* mvn archetype:generate

Edit pom as follows:

    <project xmlns="http://maven.apache.org/POM/4.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
        http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

      <groupId>myGroupId</groupId>
      <artifactId>myArtifactId</artifactId>
      <version>myVersion</version>
      <!-- <packaging>sar</packaging> -->

      <name>myProjectName</name>
      <url>http://maven.apache.org</url>

      <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
      </properties>

      <build>
        <plugins>
            <plugin>
                <groupId>com.financialforce.maven</groupId>
                <artifactId>force-maven-plugin</artifactId>
                <version>1.0.2</version>
                <configuration>
                    <deployRoot>src</deployRoot>
                </configuration>
                <executions>
                    <execution>
                        <id>force</id>
                        <phase>compile</phase>
                        <goals>
                            <goal>compile</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
      </build>
    </project>

* mvn compile
