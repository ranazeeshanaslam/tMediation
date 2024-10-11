CURRENDIR=/billing/tMediation/teles_iswitch_src_sec/
cd $CURRENDIR
ls -1 *.gz > filelist
for filename in `cat filelist`
do
gunzip $filename > temp
#mv temp $CURRENDIR/zipfiles/$filename
done

#echo "removing zip files"
#for filename in `cat filelist`
#do
#rm $filename
#mv temp $CURRENDIR/zipfiles/$filename
#done

