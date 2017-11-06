#!/bin/bash
BODY=''
LINEA=''
ID='##'
ID2='###'
ID3='-'
FIN='####'
READ=false
FINAL=false
while read -r i;
do
    LINEA=$i;
    if  [[ $i == *$ID* ]];
    then
        READ=true
    fi
    if [[ $i == *$FIN* ]];
    then
        echo -n '</ul>'
        break
    fi
    if [[ $READ == true ]];
    then
        if  [[ $i == *$ID2* ]];
        then
            if  [[ $FINAL == true ]];
            then
                echo -n '</li>'
                echo -n '</ul>'
            fi
            echo -n '<li>'
            echo -n ${LINEA:3}
            echo -n '<ul>'
            FINAL=true

        elif [[ $i == *$ID* ]];
        then
            echo -n $LINEA
            echo -n '</h2>'
            echo -n '<br/>'
            echo -n '<ul>'
        elif [[ $i == *$ID3* ]];
        then
            echo -n '<li>'
            echo -n ${LINEA:1}
            echo -n '</li>'
        fi
    fi
done < CHANGELOG.md
