Clone repo
Run 'mvn install'

Create pom.xml as below
Run 'mvn compile'

<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.financialforce.*myGroupId*</groupId>
  <artifactId>*myArtifactId*</artifactId>
  <version>*myVersion*</version>
  <!-- <packaging>sar</packaging> -->

  <name>*myName*</name>
  <url>http://maven.apache.org</url>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <build>
    <plugins>
        <plugin>
            <groupId>com.financialforce.maven</groupId>
            <artifactId>force-maven-plugin</artifactId>
            <version>1.0.1</version>
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
    <pluginManagement>
        <plugins>
            <!--This plugin's configuration is used to store Eclipse m2e settings only. It has no influence on the Maven build itself.-->
            <plugin>
                <groupId>org.eclipse.m2e</groupId>
                <artifactId>lifecycle-mapping</artifactId>
                <version>1.0.0</version>
                <configuration>
                    <lifecycleMappingMetadata>
                        <pluginExecutions>
                            <pluginExecution>
                                <pluginExecutionFilter>
                                    <groupId>
                                        com.financialforce.maven
                                    </groupId>
                                    <artifactId>
                                        force-maven-plugin
                                    </artifactId>
                                    <versionRange>
                                        [1.0-SNAPSHOT,)
                                    </versionRange>
                                    <goals>
                                        <goal>compile</goal>
                                    </goals>
                                </pluginExecutionFilter>
                                <action>
                                    <ignore></ignore>
                                </action>
                            </pluginExecution>
                        </pluginExecutions>
                    </lifecycleMappingMetadata>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>
  </build>
</project>