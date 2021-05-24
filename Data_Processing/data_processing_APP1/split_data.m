function [mac,rss,label] = split_data(imported_data)
mac=[];
rss=[];
label=[];
for i=1:length(imported_data)
    splits=split(imported_data(i));
    mac=[mac;splits(1)];
    rss=[rss;splits(2)];
    label=[label;splits(3)];
end
end

