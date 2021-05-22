function [f_normalized] = normalize(f)
%UNTITLED 此处显示有关此函数的摘要
%   此处显示详细说明
f_normalized=f;
total=sum(f);
for i=1:size(f,2)
    f_normalized(i)=f(i)/total;
end

end

