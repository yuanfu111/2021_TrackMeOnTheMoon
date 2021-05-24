function [f_cleaned] = clean_f(f_)
%UNTITLED2 此处显示有关此函数的摘要
%   此处显示详细说明
f_cleaned=f_;
for i=1:length(f_)
    if(f_(i)<0.0001)
       f_cleaned(i)=0;
    end
end
end

