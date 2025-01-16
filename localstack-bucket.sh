#!/bin/bash

# Create the 'local-development' bucket
awslocal s3api create-bucket \
    --bucket local-development \
    --create-bucket-configuration LocationConstraint=ap-southeast-1 \
    --region ap-southeast-1

# Create the 'quarantine' bucket
awslocal s3api create-bucket \
    --bucket quarantine-bucket \
    --create-bucket-configuration LocationConstraint=ap-southeast-1 \
    --region ap-southeast-1

# Create the 'clean' bucket
awslocal s3api create-bucket \
    --bucket clean-bucket \
    --create-bucket-configuration LocationConstraint=ap-southeast-1