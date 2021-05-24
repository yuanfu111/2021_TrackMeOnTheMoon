%clc
clear all

%Read the data
% files=dir('*RSS*.txt');
% data_read=[];
% num_file =length(files);
% sample_size=num_file*4;
% for i=1:num_file
%     sub_data=string(importdata(files(i).name));
%     data_read=[data_read;sub_data];
% end
% [raw_macs,raw_rsses,raw_labels]=split_data(data_read);
% data=[raw_macs,raw_rsses,raw_labels];

%Filter out data with small value
% raw_sample_size=length(raw_labels);
% threshold=-50;
% macs=[];
% rsses=[];
% labels=[];
% for i=1:raw_sample_size
%     if str2double(raw_rsses(i))>=threshold
%         macs=[macs; raw_macs(i)];
%         rsses=[rsses; raw_rsses(i)];
%         labels=[labels; raw_labels(i)];
%     end
% end
% uniqe_mac = unique(macs).';
% num_line=length(labels);
% num_feature=length(uniqe_mac);


%Rearrange the data
% labels_cleaned=zeros(num_line,1);
% % A->0 B->1 C->2 D->3
% for i=1:num_line
%     switch labels(i)
%         case "A"
%             labels_cleaned(i)=0;
%         case "B"
%             labels_cleaned(i)=1;
%         case "C"
%             labels_cleaned(i)=2;
%         case "D"
%             labels_cleaned(i)=3;
%     end
% end
% data_cleaned=[macs rsses labels_cleaned];
%data_cleaned=[data_cleaned,zeros(length(labels),1)];
% mac1 mac2 .....macn label
% for i=1:num_line
%     line=zeros(1,num_feature+1);
%     %find the index of the mac in the feature space
%     index=findFeatureIndex(uniqe_mac,macs(i));
%     if index >=0
%         data_cleaned(i,index)=rsses(i);
%     end
%     data_cleaned(i,end)=labels_cleaned(i);
% end
% sample=zeros(sample_size,num_feature+1);
% for i=1:sample_size
%     
% end
 sel_feature_num=25;
 data=load("data_loc_all.txt")
 %macs=load("chosen_macs.txt")
 randIndex = randperm(size(data,1));
 data=data(randIndex,:);
 sample = data(:,1:end-1);
 target =data(:,end);
 [idx,scores] = fscmrmr(sample,target);
 %classificationLearner
%  feature_selected=idx(1:sel_feature_num)
 
 bar(scores(idx))
xlabel('Predictor rank')
ylabel('Predictor importance score')
 %% Clean Features
  
%  data_selected=data(:,idx(1:sel_feature_num))
%  data_selected=[data_selected target]
%   save('data_loc.txt','data_selected','-ascii');
%   idx(1:sel_feature_num)
%   PYTHON_idx=idx(1:sel_feature_num)-1
  
