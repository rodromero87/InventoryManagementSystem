#!/usr/bin/env bash
set -euo pipefail

echo "[LocalStack] Criando SNS topic e 10 filas SQS..."

TOPIC_NAME="inventory-updates"
TOPIC_ARN=$(awslocal sns create-topic --name "$TOPIC_NAME" --query 'TopicArn' --output text)
echo "SNS Topic: $TOPIC_ARN"

for i in $(seq -w 01 10); do
  QNAME="inv-loja-$i"
  QURL=$(awslocal sqs create-queue --queue-name "$QNAME" \
           --attributes MessageRetentionPeriod=345600 \
           --query 'QueueUrl' --output text)
  QARN=$(awslocal sqs get-queue-attributes --queue-url "$QURL" \
           --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)
  echo "Criada fila $QNAME url=$QURL arn=$QARN"

  # Política (pode falhar em LS — ignorar)
  POLICY="{\"Version\":\"2012-10-17\",\"Statement\":[{\"Sid\":\"Allow-SNS-Publish\",\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":\"sqs:SendMessage\",\"Resource\":\"$QARN\",\"Condition\":{\"ArnEquals\":{\"aws:SourceArn\":\"$TOPIC_ARN\"}}}]}"
  awslocal sqs set-queue-attributes --queue-url "$QURL" --attributes Policy="$POLICY" \
    || echo "[WARN] set-queue-attributes falhou"

  # Assinatura com payload direto
  awslocal sns subscribe --topic-arn "$TOPIC_ARN" --protocol sqs \
    --notification-endpoint "$QARN" \
    --attributes RawMessageDelivery=true \
    --return-subscription-arn >/dev/null \
    || echo "[WARN] subscribe falhou para $QNAME"
done

echo "[LocalStack] SNS+SQS prontos."

