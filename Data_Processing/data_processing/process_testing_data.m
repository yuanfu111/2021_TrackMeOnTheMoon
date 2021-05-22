clear all
%% processing testing data
testing_data=load("testing_data.txt");
sample = testing_data(:,1:end-1);
target =testing_data(:,end);
save("testing_sample.txt",'sample','-ascii');
save("testing_target.txt",'target','-ascii');
%plot(sample(4,:))