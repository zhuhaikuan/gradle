#!/bin/bash

echo Enter username:
read username
echo Enter password:
read -s password

if [ "$username" = "secret-user" ] && [ "$password" = "secret-password" ] ; then
    echo "Welcome, $username!"
elif [ "$username" = "secret-properties-user" ] && [ "$password" = "secret-properties-password" ] ; then
    echo "Welcome, $username!"
else
    echo "Bad credentials!"
    exit 1
fi
