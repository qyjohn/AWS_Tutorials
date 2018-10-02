package main

import (
        "fmt"
        "time"
        "github.com/aws/aws-dax-go/dax"
        "github.com/aws/aws-sdk-go/aws"
        "github.com/aws/aws-sdk-go/service/dynamodb"
)

func main() {
        cfg := dax.DefaultConfig()
        cfg.HostPorts = []string{"xxxx.xxxxxx.clustercfg.dax.apse2.cache.amazonaws.com:8111"}
        cfg.Region = "ap-southeast-2"
        client, err := dax.New(cfg)
        if err != nil {
                panic(fmt.Errorf("unable to initialize client %v", err))
        }

        inp := &dynamodb.GetItemInput{
                TableName: aws.String("TryDaxGoTable"),
                Key: map[string]*dynamodb.AttributeValue{
                        "pk":   {S: aws.String("mykey")},
                        "sk":   {N: aws.String("0")},
                },
        }

        for i:=0; i<1000000; i++ {
                t1 := time.Now().UnixNano() / 1000000
                client.GetItem(inp)
                t2 := time.Now().UnixNano() / 1000000
                latency := t2 - t1
                fmt.Println(latency)
        }
}
