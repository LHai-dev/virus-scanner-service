#!/bin/bash

echo "Initializing LocalStack with required S3 buckets..."

# Create the 'local-development' bucket
awslocal s3api create-bucket \
    --bucket local-development \
    --create-bucket-configuration LocationConstraint=ap-southeast-1 \
    --region ap-southeast-1
echo "Bucket 'local-development' created."

# Create the 'quarantine' bucket
awslocal s3api create-bucket \
    --bucket quarantine-bucket \
    --create-bucket-configuration LocationConstraint=ap-southeast-1 \
    --region ap-southeast-1
echo "Bucket 'quarantine-bucket' created."

# Create the 'clean' bucket
awslocal s3api create-bucket \
    --bucket clean-bucket \
    --create-bucket-configuration LocationConstraint=ap-southeast-1 \
    --region ap-southeast-1
echo "Bucket 'clean-bucket' created."

echo "Initialization complete!"