package main

import (
        "fmt"
        "time"
        "bufio"
        "os"
        "sync"
        "github.com/aws/aws-dax-go/dax"
        "github.com/aws/aws-sdk-go/aws"
        "github.com/aws/aws-sdk-go/service/dynamodb"
)


func readLines(path string) ([]string, error) {
        file, err := os.Open(path)
        if err != nil {
            return nil, err
        }
        defer file.Close()

        var lines []string
        scanner := bufio.NewScanner(file)
        for scanner.Scan() {
            lines = append(lines, scanner.Text())
        }
        return lines, scanner.Err()
}

func test() {
        keys, err := readLines("keys.txt")
        cfg := dax.DefaultConfig()
        cfg.HostPorts = []string{"xxxx.xxxxxx.clustercfg.dax.usw2.cache.amazonaws.com:8111"}
        cfg.Region = "us-west-2"
        client, err := dax.New(cfg)
        if err != nil {
                panic(fmt.Errorf("unable to initialize client %v", err))
        }

        for i:=0; i<len(keys); i++ {
                key := keys[i]
                inp := &dynamodb.GetItemInput{
                        TableName: aws.String("objects"),
                        Key: map[string]*dynamodb.AttributeValue{
                                "__segment_internal_id":   {S: aws.String(key)},
                        },
                }

		// First read request latency
                t1 := time.Now().UnixNano() / 1000
                client.GetItem(inp)
                t2 := time.Now().UnixNano() / 1000
                first_latency := t2 - t1

		// Second read request latency
                t3 := time.Now().UnixNano() / 1000
                client.GetItem(inp)
                t4 := time.Now().UnixNano() / 1000
                second_latency := t4 - t3

		// Third read request latency
                t5 := time.Now().UnixNano() / 1000
                client.GetItem(inp)
                t6 := time.Now().UnixNano() / 1000
                third_latency := t6 - t5

		// Average read latency
		average_latency := (first_latency + second_latency + third_latency) / 3
                fmt.Println(first_latency, "\t", second_latency, "\t", third_latency, "\t", average_latency)
        }
}


func main() {
        concurrency := 100
        var wg sync.WaitGroup
        wg.Add(concurrency)

        // Start the threads
        for i:=0; i<concurrency; i++ {
                go func(i int) {
                        defer wg.Done()
                        test()
                }(i)
        }

        // Wait for the threads to finish
        wg.Wait()
}
