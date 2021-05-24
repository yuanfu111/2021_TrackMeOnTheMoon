data=load("data_trimmed.txt");

%%feature selection
sample = data(:,1:end-1);
sel_feature_num=size(sample,2);
 target =data(:,end);
 [idx,scores] = fscmrmr(sample,target);
 %classificationLearner
 feature_selected=idx(1:30)
 bar(scores(idx))
xlabel('Features')
ylabel('Importance')

