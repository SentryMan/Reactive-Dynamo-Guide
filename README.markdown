# Reactive-Dynamo-Guide

 So you want to use Reactive DynamoDB huh?
 This guide is made for you.

 It explains all the operations in the above repository.

## Dependencies

    <properties>
        <java.version>1.8</java.version>
        <version.dynamodblocal>1.12.0</version.dynamodblocal>
        <version.sqllite4java>1.0.392</version.sqllite4java>
        <version.awssdk>2.13.18</version.awssdk>
    </properties>

    <dependency>
        <groupId>org.modelmapper</groupId>
        <artifactId>modelmapper</artifactId>
        <version>2.3.0</version>
    </dependency>

    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>dynamodb-enhanced</artifactId>
        <version>${version.awssdk}</version>
    </dependency>

    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>dynamodb</artifactId>
        <version>${version.awssdk}</version>
    </dependency>

    <dependency>
        <artifactId>DynamoDBLocal</artifactId>
        <groupId>com.amazonaws</groupId>
        <version>${version.dynamodblocal}</version>
    </dependency>

    <dependency>
        <artifactId>libsqlite4java-osx</artifactId>
        <groupId>com.almworks.sqlite4java</groupId>
        <type>dylib</type>
        <version>${version.sqllite4java}</version>
    </dependency>
    <dependency>
        <artifactId>sqlite4java</artifactId>
        <groupId>com.almworks.sqlite4java</groupId>
        <version>${version.sqllite4java}</version>
    </dependency>
    <dependency>
        <artifactId>libsqlite4java-linux-amd64</artifactId>
        <groupId>com.almworks.sqlite4java</groupId>
        <scope>test</scope>
        <type>so</type>
        <version>${version.sqllite4java}</version>
        </dependency>
    <dependency>
        <artifactId>libsqlite4java-linux-i386</artifactId>
        <groupId>com.almworks.sqlite4java</groupId>
        <scope>test</scope>
        <type>so</type>
        <version>${version.sqllite4java}</version>
    </dependency>
    <dependency>
        <artifactId>sqlite4java-win32-x64</artifactId>
        <groupId>com.almworks.sqlite4java</groupId>
        <scope>test</scope>
        <type>dll</type>
        <version>${version.sqllite4java}</version>
    </dependency>
    <dependency>
        <artifactId>sqlite4java-win32-x86</artifactId>
        <groupId>com.almworks.sqlite4java</groupId>
        <scope>test</scope>
        <type>dll</type>
        <version>${version.sqllite4java}</version>
    </dependency>

## Plugins

    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <executions>
            <execution>
                <id>copy</id>
                <phase>compile</phase>
                <goals>
                    <goal>copy-dependencies</goal>
                </goals>
                <configuration>							
                    <includeScope>test</includeScope>
                    <includeTypes>so,dll,dylib</includeTypes>
                    <outputDirectory>${project.basedir}/native-libs</outputDirectory>
                </configuration>
            </execution>
        </executions>
    </plugin>

## Setup

This section explains how to setup your initial classes

1. First you're going to instantiate a `DynamoDbEnhancedAsyncClient`

      @Bean
      public DynamoDbEnhancedAsyncClient getDynamoDbEnhancedAsyncClient() {

        return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(getDynamoDbAsyncClient()).build();
      }

      public DynamoDbAsyncClient getDynamoDbAsyncClient() {
        return DynamoDbAsyncClient.builder().endpointOverride(URI.create(dynamoDBEndpoint))
            .credentialsProvider(DefaultCredentialsProvider.builder().profileName(awsProfile).build())
            .build();
      }
    ``` 
    2. 
    #Testing

