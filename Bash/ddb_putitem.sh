#!/bin/bash
date +%s.%3N
aws dynamodb put-item --table-name training --item '{"hash":{"S":"xxxx-xxxx-xxxx-xxxx"}, "sort":{"N":"12345678"}, "val":{"S":"ABCDEFG"}}' --debug
date +%s.%3N
