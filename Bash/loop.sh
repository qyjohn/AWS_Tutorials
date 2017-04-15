#
# loop.sh
# Simple bash script with parameters
# $1 is your first parameter and $2 is your second parameter
#
for i in `seq $1..$2`
do 
    echo $i
done

