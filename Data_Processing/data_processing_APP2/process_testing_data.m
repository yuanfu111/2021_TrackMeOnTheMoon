clear all
%% processing testing data
testing_data=load("testing_data.txt");
testing_sample = testing_data(:,1:end-1);
testing_target =testing_data(:,end);
save("testing_sample.txt",'testing_sample','-ascii');
save("testing_target.txt",'testing_target','-ascii');
%plot(sample(4,:))