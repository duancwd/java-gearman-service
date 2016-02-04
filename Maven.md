#Maven


# Maven #

The java-gearman-service is available through maven!

## Repository ##

### Sonatype ###
The java-gearman-service and its sub-projects are officially hosted at sonatype. Configure your project's pom to include the following repository:

```
   <!-- sonatype -->
   <repository>
      <id>sonatype</id>
      <url>https://oss.sonatype.org/content/groups/public</url>
   </repository>
```


## Java Gearman Service ##

The core project. These are main libraries for the gearman client, work, and server.

### Snapshot (Latest & Greatest) ###
If you need the very latest build and latest but fixes that have yet to be released, include the following dependency in your pom.


```
   <!-- java-gearman-service -->
   <dependency>
      <groupId>org.gearman.jgs</groupId>
      <artifactId>java-gearman-service</artifactId>
      <version>[0.7.0-SNAPSHOT,)</version>
   </dependency>
```


## Example POM ##

pom.xml
```
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

   <modelVersion>4.0.0</modelVersion>
   <groupId>com.company.myproject</groupId>
   <artifactId>myproject</artifactId>
   <version>0.0.0</version>
   <packaging>jar</packaging>

   <build>   

      <plugins>

         <!-- compile -->
         <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.0</version>
            <configuration>
               <source>1.7</source>
               <target>1.7</target>
            </configuration>
         </plugin>

      </plugins>
		
   </build>
	
   <dependencies>

      <!-- java-gearman-service -->
      <dependency>
         <groupId>org.gearman.jgs</groupId>
         <artifactId>java-gearman-service</artifactId>
         <version>[0.7.0-SNAPSHOT,)</version>
      </dependency>
	
   </dependencies>

   <repositories>

      <!-- sonatype -->
      <repository>
         <id>sonatype</id>
         <url>https://oss.sonatype.org/content/groups/public</url>
      </repository>

   </repositories>

</project>
```